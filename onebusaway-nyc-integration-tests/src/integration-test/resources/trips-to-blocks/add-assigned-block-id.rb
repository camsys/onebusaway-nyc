# irb_context.echo ^= true
require 'awesome_print'
require 'CSV'

raise "Add INPUT= and OUTPUT=" unless ENV['INPUT'] && ENV['OUTPUT']

# load traces to bundles mapping
traces = CSV.read('traces-to-bundles.txt', {col_sep: "\t", headers: true})
traces = traces.inject({}) {|m, v| m[v['trace']] = v['bundle']; m}

tracefile = File.basename(ENV["INPUT"])

# loads trips to blocks mapping for the tracefile's bundle
trips_to_blocks = CSV.read(traces[tracefile] + '.txt', {col_sep: "\t"}).inject({}) do |m, row|
  m[row[1]] = row[2]
  # Also store a copy stripping off the agency prefix
  # e.g. MTA NYCT_5882686-LGPB4-LG_B4-Saturday-11" => "5882686-LGPB4-LG_B4-Saturday-11"
  m[row[1].split(/_/, 2)[1]] = row[2]
  m
end

c = CSV.read(ENV["INPUT"], {headers: true})
assigned_block_id = []
c.each do |r|
  assigned_block_id << trips_to_blocks[r['actual_trip_id']] || ''
end
c['assigned_block_id'] = assigned_block_id
output = CSV.open(ENV['OUTPUT'], "w", {write_headers: true})
output << c.headers
c.each do |r|
  output << r
end
output.close()
# irb_context.echo ^= true
