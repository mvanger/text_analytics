require 'stanford-core-nlp'

StanfordCoreNLP.use :english

f = File.open("classbios.txt", "r")
text = ""
f.each_line do |line|
  text += line
end
f.close

# Split file into individual bios
t = text.split(/\d\d\s-+|-+\sNEW\sCOHORT\s-+|-{21}/)
# Get rid of the first empty string
t.shift

# Load some stuff. Only need :pos right now though
pipeline =  StanfordCoreNLP.load(:tokenize, :ssplit, :pos, :lemma, :parse, :ner, :dcoref)
first_bio = StanfordCoreNLP::Annotation.new(t[0].strip)
pipeline.annotate(first_bio)

# Get parts of speech for the sentences and tokens
first_bio.get(:sentences).each do |sentence|
  sentence.get(:tokens).each do |token|
    puts token.get(:value).to_s + " " + token.get(:part_of_speech).to_s
  end
end