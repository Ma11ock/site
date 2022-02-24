Rails.application.routes.draw do
  root 'posts#index'

  # Blog.
  get '/posts', to: 'posts#blog_index'
  get '/posts/:url', to: 'posts#show'
  # Hidden blog.
  get '/esoteric', to: 'posts#esoteric'
  get '/esoteric/:url', to: 'posts#esoteric_show'
  # Front page posts
  get '/blerbs', to: 'posts#blerbs'
  get '/links', to: 'posts#links'
  get '/:url', to: 'posts#front_show'

end
