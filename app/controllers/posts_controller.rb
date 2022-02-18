
class PostsController < ApplicationController
  def index
  end

  def blog_index
  end

  def esoteric
  end

  def show
    @post = Post.find(params[:id]) rescue not_found
  end
end
