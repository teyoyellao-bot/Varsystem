
package varsystem.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    public String name, email, username, password,
            phone = "", location = "", vehicle = "",
            birthday = "",
            license = "Not added yet",
            role = "User", status = "Active",
            shop = "", avatar = "";

    public LocalDateTime joined = LocalDateTime.now();

    public boolean notif = true, loc = true, remind = true;

    // PASSWORD RESET TRACKING
    // 0 = no successful reset yet
    // 1 or more = must use Admin-assisted recovery
    public int passwordResetCount = 0;
}
