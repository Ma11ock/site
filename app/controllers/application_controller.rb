class ApplicationController < ActionController::Base
  def not_found
    raise ActionController::RoutingError.new('Not Found')
  end

  def self.random_comment
    File.join('shared', ['sneed', 'industrial_society', 'anime', 'choppa',
                         'france', 'gigachad', 'peter', 'shrek', 'sneed_img',
                         'tanku', 'toem', 'troll', 'virus_exe', 'windows'].sample)
  end
end
