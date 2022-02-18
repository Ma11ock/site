
#post.content.attach(io: File.open('/home/ryan/TODO.org'), filename: "TODO.org", content_type: 'text')
Dir.glob("#{Rails.root}/app/orgs/*.org") do |filename|
  post = Post.new
  post.content.attach(io: File.open(filename, "r:UTF-8"), filename: File.basename(filename),
                      content_type: 'text')
  post.save
end
