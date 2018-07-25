package org.mozilla.focus.content;

/**
 * Created by mozillabeijing on 2018/7/25.
 */

public class Content {
    private long id;
    private String title;
    private String url;
    private String content;
    private String source;
    private String date;
    private String imgUrl;

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContent() { return this.content; }

    public void setContent(String content) { this.content = content; }

    public String getSource() { return this.source; }

    public void setSource(String source) { this.source = source; }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getImgUrl() {
        return this.imgUrl;
    }

    public void setImgUrl(String imageUrl) {
        this.imgUrl = imageUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Content) {
            if (((Content) obj).getId() == this.getId()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
