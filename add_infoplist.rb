require 'xcodeproj'
project_path = 'ios/FamWake.xcodeproj'
project = Xcodeproj::Project.open(project_path)
target = project.targets.first

# Add InfoPlist.strings VariantGroup
group = project.main_group.find_subpath('FamWake/Resources', true)
variant_group = group.children.find { |c| c.name == 'InfoPlist.strings' }
if !variant_group
  variant_group = project.new(Xcodeproj::Project::Object::PBXVariantGroup)
  variant_group.name = 'InfoPlist.strings'
  group.children << variant_group
  # Add to build phase
  resources_build_phase = target.resources_build_phase
  resources_build_phase.add_file_reference(variant_group)
end

# Add languages
Dir.glob('ios/FamWake/Resources/*.lproj').each do |lproj|
  lang = File.basename(lproj, '.lproj')
  file_path = "ios/FamWake/Resources/#{lang}.lproj/InfoPlist.strings"
  unless variant_group.children.any? { |c| c.name == lang }
    file_ref = variant_group.new_reference(file_path)
    file_ref.name = lang
  end
end

project.save
