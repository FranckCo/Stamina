##GAMSO

The model is read from the "GAMSO v1.0" document published on the UNECE wiki which was submitted to public review in March 2016.

To minimize manual manipulation, text and model structure are extracted from the document using Apache POI (http://poi.apache.org), and more specifically the XWPF component since the document in in .docx format. With XWPF, it is easy to read the document as a list of paragraph objects from which the text can be extracted.
