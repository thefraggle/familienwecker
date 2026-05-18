require 'xcodeproj'

project_path = 'ios/FamWake.xcodeproj'
project = Xcodeproj::Project.open(project_path)

target = project.targets.find { |t| t.name == 'FamWake' }

resources_group = project.main_group.find_subpath(File.join('FamWake', 'Resources'), true)
file_ref = resources_group.new_reference('alarm_default.mp3')

target.add_resources([file_ref])

project.save
puts "Added alarm_default.mp3 to FamWake target"
