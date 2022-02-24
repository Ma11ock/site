#!/usr/bin/env zsh

# TODO get nginx to serve files in /public
# TODO rake db:create RAILS_ENV=production
# TODO rake db:migrate RAILS_ENV=production
export RAILS_ENV=production
bundle install
bundle exec rake assets:precompile
bundle exec rake assets:clean
bundle exec rake db:migrate
