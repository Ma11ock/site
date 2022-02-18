Rails.application.routes.draw do
  root 'posts#index'

  get '/posts', to: 'posts#blog_index'
  get '/esoteric', to: 'posts#esoteric'
  get '/posts/:id', to: 'posts#show'
  get '/404', to: 'errors#not_found'
  get '/500', to: 'errors#not_found'
  get '/422', to: 'errors#not_found'
end
