# API Extractor Util

This util helps extract API definitions and models from an open API spec. All you need do is provide the file and the APIs you are interested in.

For now it will print the result to your console.

## How to use
1. Put the open api YAML or JSON downloaded in the resources folder
2. Goto [Constants](src/main/java/com/chrisworks/ing/openapi/extractorutil/Constants.java), specify:
    - The name of the file `(SWAGGER_FILE_NAME)`
    - The list of the APIs you want to extract from it `(ENDPOINTS_OF_INTEREST)`
3. Goto [ExtractorUtil](src/main/java/com/chrisworks/ing/openapi/extractorutil/ExtractorUtil.java), and run the main method
4. Copy output from your console, to an empty file with the right file extension (i.e `.json` or `.yaml`)
5. After copying the output and placing in a new file where you want, remember to format it `(cmd + alt + shft + L)`

We may improve on this later. Cheers