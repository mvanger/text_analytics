str = ""

# Read in data
f = File.open("classbios.txt", "r")
f.each_line do |l|
  str = str + l
end
f.close

# Clean data
str.gsub!(/[0-9]{2}\s-{2,}/, ' ')
str.gsub!(/(--)+/, ' ')
str.gsub!(/\n+/, ' ')
str.gsub!(/\r+/, ' ')
str.downcase!
split = str.split(/\.\s|!\s|\?\s/)

# Come up with some regexes
year_regex = /[^0-9](19\d{2}|2[0-1]{2}[0-5])[^0-9]/
month_regex = /([^a-z]|^)((january|february|march|april|may|june|july|august|september|october|november|december)|(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec))[^a-z]/
day_regex = /([^a-z]|^)((monday|tuesday|wednesday|thursday|friday|saturday|sunday)|(mon|tues|wed|thurs|fri|sat|sun))[^a-z]/
mmddyy_regex = /([0-9]|1[0-2])\/([0-2]{0,1}[0-9]{0,1}|30|31)\/((19\d{2}|2[0-1]{2}[0-5])|[0-9]{2}(\D|$))/
rel_regex = /today|yesterday|tomorrow/
this_regex = /this\s(week|month|year)/
last_regex = /last\s(week|month|year)/
time_regex = /(1[0-9]|2[0-3]|(^|\D)[0-9]):([0-5][0-9]){1}/
oclock_regex = /(^|\D)([1-9]|10|11|12)\so'{0,1}clock/
am_pm_regex = /(^|\D)([1-9]|10|11|12)\s(am|pm)($|\W)/

# Array of each individual regex
array_of_all = [year_regex, month_regex, day_regex, mmddyy_regex, rel_regex, this_regex, last_regex, time_regex, oclock_regex, am_pm_regex]

# Combine them all
all_regex = /([^0-9](19\d{2}|2[0-1]{2}[0-5])[^0-9])|last\s(week|month|year)|([^a-z]|^)((january|february|march|april|may|june|july|august|september|october|november|december)|(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec))[^a-z]|([^a-z]|^)((monday|tuesday|wednesday|thursday|friday|saturday|sunday)|(mon|tues|wed|thurs|fri|sat|sun))[^a-z]|([0-9]|1[0-2])\/([0-2]{0,1}[0-9]{0,1}|30|31)\/((19\d{2}|2[0-1]{2}[0-5])|[0-9]{2}(\D|$))|(1[0-9]|2[0-3]|(^|\D)[0-9]):([0-5][0-9]){1}|(^|\D)([1-9]|10|11|12)\so'{0,1}clock|(^|\D)([1-9]|10|11|12)\s(am|pm)($|\W)/

# Output some results
f = File.open("classbios_timepoints.txt", "w")
split.each do |s|
  f.write(s.strip + "\n") if s.match(all_regex)
end
f.close

split.each do |s|
  array_of_all.each do |r|
    puts s.match(r).to_s.strip if s.match(r)
  end
end