gsbpm <- VCorpus(DirSource(encoding = "UTF-8"), readerControl = list(language = "en"))

gsbpm <- tm_map(gsbpm, content_transformer(tolower))
gsbpm <- tm_map(gsbpm, removeWords, stopwords("english"))

removeDoubleQuotations <- function(x) gsub("“”", "", x)
gsbpm <- tm_map(gsbpm, removePunctuation)
gsbpm <- tm_map(gsbpm, content_transformer(removeDoubleQuotations))

gsbpm <- tm_map(gsbpm, stripWhitespace)
gsbpm <- tm_map(gsbpm, removeNumbers)

library(SnowballC)
gsbpmLem <- tm_map(gsbpm, stemDocument)
tdMatrix <- TermDocumentMatrix(gsbpm)

library(ggplot2)
termFrequency <- rowSums(as.matrix(tdMatrix))
wfU <- data.frame(word=names(termFrequency), freq=termFrequency)
wfU2<-wfU <- subset(wfU, freq>10)
wfU2$word <- factor(wfU$word, levels = wfU[order(-wfU$freq), "term"])
ggplot(data=wfU2,aes(word, freq)) + geom_bar(stat="identity") + theme(axis.text.x=element_text(angle=45, hjust=1))

library(wordcloud)
pal <- brewer.pal(9, "BuGn")
pal2 <- pal[-(1:2)]
m <- as.matrix(tdMatrix)
rownames(m) <-as.vector(stemCompletion(rownames(tdMatrix),dictionary=gsbpm, type="prevalent"))
v <- sort(rowSums(m), decreasing=TRUE)
myNames <- names(v)
d <- data.frame(word=myNames, freq=v)
wordcloud(d$word, d$freq, min.freq=5,color=pal2,random.order=FALSE)

# See: https://georeferenced.wordpress.com/2013/01/15/rwordcloud/