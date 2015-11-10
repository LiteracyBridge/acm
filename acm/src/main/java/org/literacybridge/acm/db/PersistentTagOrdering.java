package org.literacybridge.acm.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@NamedQueries({
    @NamedQuery(name = "PersistentTag.findAll", query = "select o from PersistentTag o")
})
@Table(name = "t_audioitem_has_tag")
@IdClass(PersistentTagOrdering.PrimaryKey.class)
/**
 * @deprecated: We're removing Derby DB from the ACM and are switching to a Lucene index
 *              for storing and searching all metadata.
 */
@Deprecated
class PersistentTagOrdering extends PersistentObject {
    @ManyToOne
    @JoinColumn(name="audioitem", nullable = false)
    @Id
    private PersistentAudioItem audioitem;

    @ManyToOne
    @JoinColumn(name="tag", nullable = false)
    @Id
    private PersistentTag tag;

    @Column(name="ordering", nullable = false)
    private Integer position;

    Integer getPosition() {
        return position;
    }

    void setPosition(int position) {
        this.position = position;
    }

    @Override
    public Object getId() {
        return new PrimaryKey(this);
    }

    @Override
    public String toString() {
        return "TagOrdering[audioitem=" + audioitem.getUuid() + ",tag=" + tag.getName() + ",pos=" + position + "]";
    }

    public static class PrimaryKey {
        private Integer audioitem;
        private Integer tag;

        private PrimaryKey(PersistentTagOrdering ordering) {
            this(ordering.audioitem.getUuid(), ordering.tag);
        }

        private PrimaryKey(String audioItemUuid, PersistentTag tag) {
            this.audioitem = PersistentAudioItem.uidToId(audioItemUuid);
            this.tag = tag.getId();
        }
    }

    static PersistentTagOrdering getFromDatabase(String audioItemUuid, PersistentTag tag) {
        return PersistentQueries.getPersistentObject(PersistentTagOrdering.class, new PrimaryKey(audioItemUuid, tag));
    }
}
