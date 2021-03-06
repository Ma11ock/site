# Makefile for Ryan's Blog
# Adapted from https://opensource.com/article/20/3/blog-emacs
# See: https://gitlab.com/psachin/psachin.gitlab.io

.PHONY: all publish publish_no_init

all: publish

publish: publish.el
	@echo "Publishing... with current Emacs configurations."
	emacs --batch --load publish.el --funcall org-publish-all

force: publish.el
	@echo "Publish... with configuration and force."
	emacs --batch --load publish.el -e '(org-publish-all t)'

publish_no_init: publish.el
	@echo "Publishing... with --no-init."
	emacs --batch --no-init --load publish.el --funcall org-publish-all

clean:
	@echo "Cleaning up.."
	@rm -rvf *.elc
	@rm -rvf public
	@rm -rvf ~/.org-timestamps/*
