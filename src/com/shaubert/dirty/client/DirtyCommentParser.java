package com.shaubert.dirty.client;

import com.shaubert.blogadapter.client.DataLoaderRequest;
import com.shaubert.blogadapter.client.PagerDataParserResult;
import com.shaubert.blogadapter.client.Parser;
import com.shaubert.blogadapter.client.ParserResultList;
import com.shaubert.dirty.client.HtmlTagFinder.AttributeWithValue.Constraint;
import com.shaubert.dirty.client.HtmlTagFinder.Callback;
import com.shaubert.dirty.client.HtmlTagFinder.Rule;
import com.shaubert.dirty.client.HtmlTagFinder.TagNode;
import com.shaubert.util.Shlog;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DirtyCommentParser extends HtmlParser implements Parser {

    private static final String COMMENTS_HTML_PARSING_TIMER = "comments receiving and html parsing time";
    
    private static final String COMMENT_DIV_CLASS = "comment indent_";
    private static final String POST_BODY_CLASS_HEADER = "b-post_comments_page_header";
    private static final String POST_BODY_CLASS_FOOTER = "dt";

    private static final Shlog SHLOG = new Shlog(DirtyPostParser.class.getSimpleName());

    private DirtyRecordParser helperParser;

    private DirtyPost dirtyPost;
    private TagNode headerBody;

    private PagerDataParserResult<DirtyComment> result;
    
    public DirtyCommentParser() {
        helperParser = new DirtyRecordParser();
        dirtyPost = new DirtyPost();
    }

    public DirtyPost getDirtyPost() {
        return dirtyPost;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ParserResultList<DirtyComment> parse(DataLoaderRequest request, InputStream inputStream) throws IOException {
        this.result = new PagerDataParserResult<DirtyComment>();
        this.result.setResult(new ArrayList<DirtyComment>());
        
        addTagFinder(new HtmlTagFinder(new Rule("div")
                .withAttributeWithValue("class", COMMENT_DIV_CLASS, Constraint.STARTS_WITH)
                .withAttribute("id"), commentCallback));

        addTagFinder(new HtmlTagFinder(new Rule("div")
                .withAttributeWithValue("class", POST_BODY_CLASS_HEADER, Constraint.STARTS_WITH),
                postHeaderBodyCallback));
        addTagFinder(new HtmlTagFinder(new Rule("div")
                .withAttributeWithValue("class", POST_BODY_CLASS_FOOTER, Constraint.STARTS_WITH),
                postFooterBodyCallback));

        SHLOG.d("start loading and parsing html data");
        SHLOG.resetTimer(COMMENTS_HTML_PARSING_TIMER);
        parse(inputStream);
        SHLOG.logTimer(COMMENTS_HTML_PARSING_TIMER);
        
        return result;
    }

    private Callback commentCallback = new Callback() {
        @Override
        public void onTagFounded(HtmlTagFinder finder, TagNode foundTag) {
            parseComment(foundTag);
        }
    };
    private Callback postHeaderBodyCallback = new Callback() {
        @Override
        public void onTagFounded(HtmlTagFinder finder, TagNode foundTag) {
            headerBody = foundTag;
        }
    };
    private Callback postFooterBodyCallback = new Callback() {
        @Override
        public void onTagFounded(HtmlTagFinder finder, TagNode foundTag) {
            TagNode node = new TagNode("div", null);
            node.addChild(headerBody);
            node.addChild(foundTag);
            helperParser.parseBody(node, dirtyPost);
        }
    };

    protected void parseComment(TagNode tag) {
        final DirtyComment comment = new DirtyComment();
        long commentId = Long.parseLong(tag.getAttributes().getValue("", "id"));
        SHLOG.d("parsing comment " + commentId);
        comment.setServerId(commentId);
        String indentLevelStr = tag.getAttributes().getValue("", "class").split(" ")[1];
        comment.setIndentLevel(Integer.parseInt(indentLevelStr.substring(7)));
        
        TagNode body = tag.getNotContentChilds().get(0).getNotContentChilds().get(0);
        helperParser.parseBody(body, comment);

        TagNode info = tag.getNotContentChilds().get(0).getNotContentChilds().get(1);
        List<TagNode> authorTags = info.findAll(new Rule("a").withAttributeWithValue("class", "c_user"));
        if (authorTags.size() == 1) {
            TagNode authorTag = authorTags.get(0);
            comment.setAuthor(authorTag.getChilds().get(0).getText());
            String href = authorTag.getAttributes().getValue("", "href");
            if (href.startsWith("http")) {
                comment.setAuthorLink(href);
            } else {
                comment.setAuthorLink("http://www.d3.ru" + href);
            }
        }
        List<TagNode> dateSpans = info.findAll(new Rule("span").withAttribute("data-epoch_date"));
        if (dateSpans.size() == 1) {
            TagNode dateSpan = dateSpans.get(0);
            String postDate = dateSpan.getAttributes().getValue("data-epoch_date");
            comment.setCreationDate(helperParser.parseEpochDate(postDate));
        }
        List<TagNode> voteTags = info.findAll(new Rule("div").withAttributeWithValue("class", "vote c_vote"));
        if (!voteTags.isEmpty()) {
			comment.setVotesCount(Integer.parseInt(voteTags.get(0).getNotContentChilds().get(0).getChilds().get(0).getText()));
        } else {
        	comment.setVotesCount(0);
        }
        comment.setOrder(result.getResult().size());
        result.getResult().add(comment);
    } 
}
