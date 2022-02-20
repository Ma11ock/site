require_relative "boot"

require "rails/all"
require 'cgi'
require 'pathname'
# Require the gems listed in Gemfile, including any gems
# you've limited to :test, :development, or :production.
Bundler.require(*Rails.groups)

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
  title = get_org_path file_path
  begin
    post = Post.where(title: title).sole
    post.destroy 
  rescue => error
    # Nothing to destroy
  end
end

def add_org_to_db(file_path)
  # TODO actually get the title of the document.
  # TODO get a description
  title = org_get_title file_path
  begin
    post = Post.where(title: title).sole
    # Reset content and title.
    post.body = Orgmode::Parser.new(File.read(file_path)).to_html 
    post.title = title
  # TODO add more rescues for different errors 
  rescue => error
    # File does not exist, create it.
    dir_name = File.dirname(get_org_path file_path)
    Post.new(title: title, description: '',
             where: dir_name == '.' ? '' : dir_name,
             url: File.basename(file_path, '.*'),
             body: Orgmode::Parser.new(File.read(file_path)).to_html).save 
  end
end

module Site
  class Application < Rails::Application
    # Initialize configuration defaults for originally generated Rails version.
    config.load_defaults 7.0

    # Handle 404's by myself
    config.exceptions_app = self.routes
    config.public_file_server.enabled = true

    # Init the database
    config.after_initialize do
      # Do change in orgs file.
      listener = Listen.to(org_dir) do |modified, added, removed|
        modified.each { |x| add_org_to_db x }
        added.each { |x| add_org_to_db x }
        removed.each { |x| remove_org_from_db x }
      end
      listener.start
    end
  end
end
