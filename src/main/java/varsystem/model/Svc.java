
package varsystem.model;

import java.io.Serializable;

/** A service offered in the system catalog. */
public class Svc implements Serializable {

    private static final long serialVersionUID = 1L;

    public String name, category, desc;
    public int base;
    public boolean active = true;
}
