package dan.client;

import java.util.List;

/**
 * @author Daneel S. Yaitskov
 */
public class Page<T> {
    private List<T> items;
    private long pages;

    public Page(List<T> items, long total, int pageSize) {
        this.items = items;
        pages = total / pageSize + (total % pageSize > 0 ? 1 : 0);
    }

    public Page(List<T> items, long pages) {
        this.items = items;
        this.pages = pages;
    }

    public List<T> getItems() {
        return items;
    }

    public long getPages() {
        return pages;
    }
}
