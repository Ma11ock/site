
class PostsController < ApplicationController
  def index
  end

  def blog_index
  end

  def esoteric
  end

  def show
    @post = Post.find(params[:url]) rescue not_found
  end

  def front_show
    @post = Post.find_by(url: params[:url]) rescue not_found
    render 'show'
  end

  def esoteric_show
  end
end
