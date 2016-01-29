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
tdMatrix <- TermDocumentMatrix(gsbpmLem)
td.mat <- as.matrix(tdMatrix)

# if necessary: install.packages('lsa')
library(lsa)
td.mat.lsa <- lw_bintf(td.mat) * gw_idf(td.mat)
lsaSpace <- lsa(td.mat.lsa)
dist.mat.lsa <- dist(t(as.textmatrix(lsaSpace)))

# Classical multidimentional scaling
fit <- cmdscale(dist.mat.lsa, eig = TRUE, k = 2)
points <- data.frame(x = fit$points[, 1], y = fit$points[, 2])

qplot(x, y, data = points, geom = "point", alpha = I(1/5))

# See: http://meefen.github.io/blog/2013/03/11/analyze-text-similarity-in-r-latent-semantic-analysis-and-multidimentional-scaling/