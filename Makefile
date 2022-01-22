# Makefile for Ryan's Blog

.PHONY: all publish publish_no_init

all: publish

publish: 
	@echo "Publishing normal configuration"
	tsc

run: publish
	@exec node bin/server.js
