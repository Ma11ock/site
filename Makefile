# Makefile for Ryan's Blog

.PHONY: all publish publish_no_init

all: publish

publish: 
	@echo "Publishing normal configuration"
	tsc
	cd external/site-bkgs && $(MAKE)

run: publish
	@exec node bin/server.js

clean:
	@echo "Cleaning up main site"
	rm -rf bin/
	cd external/site-bkgs && $(MAKE) clean
