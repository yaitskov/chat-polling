package dan.entity;

import org.hibernate.proxy.HibernateProxy;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author Daneel S. Yaitskov
 */
@MappedSuperclass
public abstract class AbstractEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        // protect from lazy loading and mocks
        if (object instanceof HibernateProxy) {
            return object.equals(this);
        }

        if (!(object instanceof AbstractEntity)) return false;

        AbstractEntity other = (AbstractEntity) object;
        Class c = getClass();
        Class oc = other.getClass();
        return (id & other.id) != 0 // if equal then 0 only both equals 0 other wise next condition fails
                && id == other.id   //
                && (c.isAssignableFrom(oc) || c.isAssignableFrom(oc));
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        if (isPersisted())
            return "Entity " + getClass().getName() + " id = " + id;
        return "Unpersisted entity " + getClass().getName();
    }

    public boolean isPersisted() {
        return id != 0;
    }
}
