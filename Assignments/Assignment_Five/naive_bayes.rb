require 'stuff-classifier'
require 'csv'
require 'pry'

cls = StuffClassifier::Bayes.new("Africa or Asia or Oceania or Europe or North America or South America")

CSV.foreach('countries_and_capitals.csv', headers: true) do |row|
  cls.train(row[0].gsub(/\s+/,"_").downcase.to_sym, row[4])
end

matrix = []

CSV.foreach('countries_and_capitals.csv', headers: true) do |row|
  matrix << [row[0].gsub(/\s+/,"_").downcase.to_sym, cls.classify(row[4])]
end

