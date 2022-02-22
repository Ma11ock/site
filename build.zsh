#!/usr/bin/env zsh

# TODO get nginx to serve files in /public
# TODO rake db:create RAILS_ENV=production
# TODO rake db:migrate RAILS_ENV=production
export RAILS_ENV=production
bin/bundle install
bin/bundle exec rake assets:precompile
bin/bundle exec rake assets:clean
bin/bundle exec rake db:migrate
