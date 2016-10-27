# This small utility scans the list of files in src/integration-test/resources/traces
# (assumed to all be CSVs) and produces a summary of columns present by file.
# By default the output is comma-delimited and intended to be imported into a
# spreadsheet tool, e.g.
#
# (in the root onebusaway-nyc-integration-tests folder: )
# ruby src/misc/summarize-csvs.rb > t.csv
#
# There's also a github-flavored-markdown output option:
# OUTPUT=github ruby src/misc/summarize-csvs.rb > t.md

require 'csv'
require 'awesome_print'
keys = Hash.new {|h,k| h[k] = {}}
filenames = []
Dir.glob("src/integration-test/resources/traces/*").each do |f|
  filename = File.basename(f)
  filenames << filename
  CSV.foreach(f, headers: true) do |row|
    row = row.to_hash
    filtered = row.select{|k, v| v!=nil}
    filtered.keys.each do |column|
      keys[column][filename] = true
    end
  end
end
case ENV['OUTPUT']
when 'github'
  puts ["Filename", keys.keys.sort].join('|')
  puts (['---'] * (keys.keys.size + 1)).join('|')
  filenames.each do |filename|
    puts [filename, keys.keys.sort.collect{|column| (keys[column][filename] ? 'X' : '')}].flatten.join('|')
  end
else
  puts ["", keys.keys.sort].join(',')
  filenames.each do |filename|
    puts [filename, keys.keys.sort.collect{|column| (keys[column][filename] ? 'X' : '')}].flatten.join(',')
  end
end
