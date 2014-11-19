require 'stanford-core-nlp'

StanfordCoreNLP.use :english

f = File.open("wsj_0063.txt", "r")
text = ""
f.each_line do |line|
  text += line
end
f.close

# Load some annotators
pipeline =  StanfordCoreNLP.load(:tokenize, :ssplit, :pos, :lemma, :parse, :ner)
annotated_text = StanfordCoreNLP::Annotation.new(text)
pipeline.annotate(annotated_text)

# Get parts of speech for the sentences and tokens
annotated_text.get(:sentences).each do |sentence|
  puts sentence.to_s
end

annotated_text.get(:sentences).each do |sentence|
  sentence.get(:tokens).each do |token|
    puts token.get(:value).to_s + "\t\t" + token.get(:lemma).to_s + "\t\t" + token.get(:named_entity_tag).to_s
  end
end