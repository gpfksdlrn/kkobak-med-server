#!/bin/bash
# 운영 서버(EC2)에서 새 버전을 반영한다. GitHub Actions 가 SSM 으로 root 권한으로 실행한다.
# 사용법: deploy.sh <ECR 이미지 주소:태그>
#   예) deploy.sh 505906471671.dkr.ecr.ap-northeast-2.amazonaws.com/kkobak-med-server:abc1234
set -euo pipefail

IMAGE="$1"
REPO_DIR=/home/ec2-user/kkobak-med-server
COMPOSE="docker compose -f docker-compose.prod.yml"
REGISTRY="${IMAGE%%/*}"
REGION=ap-northeast-2

# 1) 배포 파일(compose, nginx 설정)도 최신으로 맞춘다. 저장소 주인이 ec2-user 라서 그 사용자로 받는다
sudo -u ec2-user git -C "$REPO_DIR" pull --ff-only

cd "$REPO_DIR/deploy"

# 2) 어떤 이미지를 쓸지 .env 에 기록한다 (나중에 손으로 compose 를 다시 띄워도 같은 버전이 뜨게)
if grep -q '^APP_IMAGE=' .env; then
  sed -i "s|^APP_IMAGE=.*|APP_IMAGE=${IMAGE}|" .env
else
  echo "APP_IMAGE=${IMAGE}" >> .env
fi

# 3) EC2 역할 권한으로 ECR 에 로그인해서 새 이미지를 받고, 바뀐 컨테이너만 다시 띄운다
aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$REGISTRY"
$COMPOSE pull app
$COMPOSE up -d --no-build --remove-orphans

# 4) 새 서버가 정상인지 확인한다 (Spring 시작에 20초 안팎 걸림)
for i in $(seq 1 30); do
  if $COMPOSE exec -T app bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /actuator/health HTTP/1.0\r\n\r\n" >&3 && cat <&3' 2>/dev/null | grep -q '"status":"UP"'; then
    echo "deploy ok: ${IMAGE}"
    # 5) 이제 안 쓰는 옛 이미지를 지워 디스크(20GB)를 아낀다
    docker image prune -af --filter "until=72h" > /dev/null
    exit 0
  fi
  sleep 3
done

echo "health check failed: ${IMAGE}" >&2
$COMPOSE logs --tail 50 app >&2
exit 1
