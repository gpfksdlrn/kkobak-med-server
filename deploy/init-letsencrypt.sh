#!/bin/bash
# 처음 한 번만 실행: Let's Encrypt 인증서를 발급받고 nginx 를 띄운다. (EC2 의 deploy 폴더에서, sudo 없이 ec2-user 로)
#
# 왜 필요한가: nginx 는 인증서 파일이 있어야 443 설정으로 켜지고,
#              certbot 은 nginx(80)가 켜져 있어야 인증서를 받을 수 있다 ("닭이 먼저냐 달걀이 먼저냐").
# 그래서: ① 임시(가짜) 인증서로 nginx 를 먼저 켜고 → ② certbot 으로 진짜 인증서 발급 → ③ nginx 다시 불러오기
#
# 사용법: ./init-letsencrypt.sh            (진짜 인증서)
#         STAGING=1 ./init-letsencrypt.sh  (연습용 인증서 — 발급 횟수 제한에 안 걸림)
set -euo pipefail

cd "$(dirname "$0")"
COMPOSE="docker compose -f docker-compose.prod.yml"
DOMAIN=$(grep '^DOMAIN=' .env | cut -d= -f2-)
LIVE=/etc/letsencrypt/live/$DOMAIN

if $COMPOSE run --rm --entrypoint sh certbot -c "test -f $LIVE/fullchain.pem && ! test -f $LIVE/.dummy" </dev/null; then
  echo "이미 $DOMAIN 인증서가 있어요. 갱신은 certbot 컨테이너가 자동으로 합니다."
  exit 0
fi

echo "① 임시 인증서 만들기 ($DOMAIN)"
$COMPOSE run --rm --entrypoint sh certbot -c "mkdir -p $LIVE && \
  openssl req -x509 -nodes -newkey rsa:2048 -days 1 -subj /CN=localhost \
  -keyout $LIVE/privkey.pem -out $LIVE/fullchain.pem && touch $LIVE/.dummy" </dev/null

echo "② nginx 켜기 (app, db 도 함께)"
$COMPOSE up -d --no-build --remove-orphans nginx </dev/null

echo "③ 임시 인증서 지우고 진짜 인증서 발급"
$COMPOSE run --rm --entrypoint sh certbot -c "rm -rf /etc/letsencrypt/live/$DOMAIN /etc/letsencrypt/archive/$DOMAIN /etc/letsencrypt/renewal/$DOMAIN.conf" </dev/null
STAGING_ARG=""
if [ "${STAGING:-0}" = "1" ]; then STAGING_ARG="--staging"; fi
$COMPOSE run --rm --entrypoint certbot certbot certonly --webroot -w /var/www/certbot \
  -d "$DOMAIN" --register-unsafely-without-email --agree-tos --no-eff-email \
  --key-type ecdsa --non-interactive $STAGING_ARG </dev/null

echo "④ nginx 가 새 인증서를 쓰게 다시 불러오고, 자동 갱신(certbot) 켜기"
$COMPOSE exec -T nginx nginx -s reload </dev/null
$COMPOSE up -d --no-build certbot </dev/null
echo "완료: https://$DOMAIN"
