package com.dao.rjobhunt.models;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Supported parser types for scraping platforms")
public enum ParserType {

    @Schema(description = "Scraper using Jsoup for static HTML parsing")
    JSOUP,

    @Schema(description = "Scraper using Selenium WebDriver for dynamic JavaScript-rendered pages")
    SELENIUM,

    @Schema(description = "Scraper using custom API integration instead of HTML scraping")
    API,

    @Schema(description = "Other or experimental parser types")
    CUSTOM

}