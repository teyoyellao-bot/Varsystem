
package varsystem.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Feedback implements Serializable {

    private static final long serialVersionUID = 1L;

    public String id;
    public String requestId;
    public String shop;
    public String userEmail;
    public String userName;
    public String comment = "";

    public int rating;

    public LocalDateTime submitted = LocalDateTime.now();

    public Feedback() {
    }

    public Feedback(
            String id,
            String requestId,
            String shop,
            String userEmail,
            String userName,
            int rating,
            String comment
    ) {
        this.id = id;
        this.requestId = requestId;
        this.shop = shop;
        this.userEmail = userEmail;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.submitted = LocalDateTime.now();
    }

    public boolean isValid() {
        return rating >= 1
                && rating <= 5
                && shop != null
                && !shop.trim().isEmpty()
                && requestId != null
                && !requestId.trim().isEmpty()
                && userEmail != null
                && !userEmail.trim().isEmpty();
    }
}
