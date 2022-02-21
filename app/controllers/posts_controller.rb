class PostsController < ApplicationController
  def index
    @posts = Post.where(where: '')
  end

  # Blog in /posts/
  def blog_index
    @posts = Post.where(where: 'posts')
  end

  def show
    @post = Post.find_by(where: 'posts', url: params[:url]) rescue not_found
  end

  # Get "rolling" front page items
  def front_show
    @post = Post.find_by(where: '', url: params[:url]) rescue not_found
    render 'show'
  end

  # Secret Blog in /esoteric/
  def esoteric
    #TODO
  end

  def esoteric_show
    @post = Post.find_by(where: 'esoteric', url: params[:url]) rescue not_found
    render 'show'
  end
end
