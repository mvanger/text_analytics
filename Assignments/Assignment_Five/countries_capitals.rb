require 'nokogiri'
require 'open-uri'
require 'csv'

doc = Nokogiri::HTML(open("http://en.wikipedia.org/wiki/List_of_countries_and_capitals_with_currency_and_language"))

# doc.css('.wikitable') gets the 7 tables
# doc.css('.wikitable')[0].children gets the African countries
# doc.css('.wikitable')[0].children[0] is unnecessary
# doc.css('.wikitable')[0].children[1] is the row for Algeria
# doc.css('.wikitable')[0].children[1].children[2].children[0].children[0].text is the text of "Algeria"
# doc.css('.wikitable')[0].children[1].children[2].children[0].children[0].attributes["href"].value is the link to Algeria
# doc.css('.wikitable')[0].children[1].children[4].children[0].text is the text of "Algiers"
# doc.css('.wikitable')[0].children[1].children[4].children[0].attributes["href"].value is the link to Algiers
# doc.css('.wikitable')[1].children[1].children[2].children[0].name is "b" or "i"
  ## The ones that start with "b" are in the UN (plus Vatican City)
  ## This gives all UN Member States plus the Vatican
  ## Capitals are either the "official" capital or the "administrative" capital

continents_array = ["Africa", "Asia", "Europe", "North America", "South America", "Oceania", "Antarctica"]

CSV.open("countries_and_capitals.csv", "wb") do |csv|
  csv << ["continent", "country", "capital", "country_text", "capital_text"]

  doc.css('.wikitable').each_with_index do |continent, cont_index|
    continent.children.each_with_index do |country, index|
      if index != 0 && country.children[2].children[0].name == "b"
        # Continent, Country, Capital
        continent_name = continents_array[cont_index]

        country_name = country.children[2].children[0].children[0].text

        capital_name = country.children[4].children[0].text

        # Opens the page for the country
        doc_country = Nokogiri::HTML(open("http://en.wikipedia.org" + country.children[2].children[0].children[0].attributes["href"].value))
        # Gets all the p tags
        country_text = ""
        doc_country.css('#mw-content-text p').each do |x|
          country_text += " " + x.text
          country_text.gsub!("\n", " ")
        end

        # Opens the page for the capital
        doc_capital = Nokogiri::HTML(open("http://en.wikipedia.org" + country.children[4].children[0].attributes["href"].value))
        # Gets all the p tags
        capital_text = ""
        doc_capital.css('#mw-content-text p').each do |x|
          capital_text += " " + x.text
          capital_text.gsub!("\n", " ")
        end

        csv << [continent_name, country_name, capital_name, country_text, capital_text]
      end
    end
  end
end

# The CSV doesn't read well in Excel or Google Docs, but looks fine when I read it in with Ruby...
