require_relative "boot"

require "rails/all"
require 'cgi'
require 'pathname'
# Require the gems listed in Gemfile, including any gems
# you've limited to :test, :development, or :production.
Bundler.require(*Rails.groups)
# <%= javascript_include_tag 'generated/movementInSquares', type: 'module', integrity: true %>
# Has to be a function because Rails.root isn't defined for constant.
def org_dir
  "#{Rails.root}/app/orgs"
end

# Get the title of a org mode file.
def org_get_title(file_path)
  title=''
  File.readlines(file_path).each do |line|
    if line.start_with?('#+TITLE:')
      line['#+TITLE:'] = ''
      title = line.strip
      break
    end
  end
  title
end

# Get the path to /app/org/f.org, chopping off anything before the org directory.
def get_org_path(file_path)
  Pathname.new(file_path).relative_path_from(org_dir).to_s
end

def remove_org_from_db(file_path)
  # Check to see if we're actually getting an org file.
  return if File.extname(file_path) != '.org'

  dir_name = File.dirname(get_org_path file_path)
  dir_name = dir_name == '.' ? '' : dir_name
  url = CGI.escape(File.basename(file_path, '.*'))
  post = Post.find_by(url: url, where: dir_name)
  post.destroy if post
end

def compile_org_file(file_path_org)
  # Generate the HTML file.
  system("emacs -q --script #{Rails.root.join('app', 'publish.el')} #{file_path_org}")
end

def update_org_db(file_path)
  # Check to see if we're actually getting an org file.
  return if File.extname(file_path) != '.org'

  # TODO get a description
  title = org_get_title file_path
  dir_name = File.dirname(get_org_path file_path)
  dir_name = dir_name == '.' ? '' : dir_name
  output_file_path = File.basename(file_path, '.org')
  url = CGI.escape(output_file_path)
  # Get the post.
  post = Post.find_by(url: url, where: dir_name)
  return if not post
  # Reset content and title.
  puts "Setting post new body page"

  compile_org_file file_path
  post.title = title
  post.save
  # TODO add more rescues for different errors
end

def add_org_to_db(file_path)
  # Check to see if we're actually getting an org file.
  return if File.extname(file_path) != '.org'

  # TODO get a description
  title = org_get_title file_path
  dir_name = File.dirname(get_org_path file_path)
  dir_name = dir_name == '.' ? '' : dir_name

  output_file_path = File.basename(file_path, '.org')
  url = CGI.escape(output_file_path)
  # Update org if it exists.
  post = Post.find_by(url: url, where: dir_name)
  return update_org_db(post) if post
  # TODO add more rescues for different errors
  # File does not exist, create it.

  compile_org_file file_path

  # TODO description.
  Post.new(title: title, description: '',
           where: dir_name,
           url: url,
           body: output_file_path + '.html').save
end

module Site
  class Application < Rails::Application
    # Initialize configuration defaults for originally generated Rails version.
    config.load_defaults 7.0
    # Handle errors myself.
    config.exceptions_app = self.routes

    config.public_file_server.enabled = true

    # Init the database
    config.after_initialize do
      # Do change in orgs file.
      listener = Listen.to(org_dir) do |modified, added, removed|
        modified.each { |x| update_org_db x }
        added.each { |x| add_org_to_db x }
        removed.each { |x| remove_org_from_db x }
      end
      listener.start
    end
  end
end
