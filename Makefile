current_dir := $(shell pwd)
name := charachorder-config

.PHONY: all watch

all:

node_modules:
	npm ci

keystore:
	sudo openssl pkcs12 -export -in /etc/letsencrypt/live/charachorder-config.com/fullchain.pem -inkey /etc/letsencrypt/live/charachorder-config.com/privkey.pem -name charachorder-config.com -out cc.pkcs12
	sudo keytool -importkeystore -deststorepass shadow -destkeystore cc.jks -srckeystore cc.pkcs12 -srcstoretype PKCS12
	sudo rm cc.pkcs12
	sudo chown $(USER) cc.jks

watch:
	rm -f public/compiled/$(name).js
	rm -f public/compiled/manifest.edn
	./node_modules/.bin/shadow-cljs watch $(name)
watch-js:
	./node_modules/.bin/babel src/js --out-dir src/gen --watch

styles:
	rm -f ./public/$(name).css
	./node_modules/.bin/sass --watch scss/$(name).scss:./public/$(name).css
styles-once:
	rm -f ./public/$(name).css
	./node_modules/.bin/sass scss/$(name).scss:./public/$(name).css

release: node_modules styles-once
	./node_modules/.bin/shadow-cljs release $(name) --pseudo-names
release-js:
	./node_modules/.bin/babel src/js --out-dir src/gen

upgrade-cljs-deps:
	clojure -M:outdated --upgrade
upgrade-node-deep-deps:
	rm -rf node_modules
	rm -f package-lock.json
	npm install --force