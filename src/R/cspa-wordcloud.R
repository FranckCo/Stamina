cspa <- VCorpus(DirSource(encoding = "UTF-8"), readerControl = list(language = "en"))
cspa <- tm_map(cspa, content_transformer(tolower))
cspa <- tm_map(cspa, removeWords, stopwords("english"))

removeDoubleQuotations <- function(x) gsub("“”", "", x)
cspa <- tm_map(cspa, removePunctuation)
cspa <- tm_map(cspa, content_transformer(removeDoubleQuotations))

cspa <- tm_map(cspa, stripWhitespace)
cspa <- tm_map(cspa, removeNumbers)

wordcloud(cspa, scale=c(5,0.5), max.words=100, random.order=FALSE, rot.per=0.35, use.r.layout=FALSE, colors=brewer.pal(8, "Dark2"))