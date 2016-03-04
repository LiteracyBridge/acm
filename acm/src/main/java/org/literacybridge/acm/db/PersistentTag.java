package org.literacybridge.acm.db;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.Playlist;

import com.google.common.collect.Lists;

@Entity
@NamedQueries({
    @NamedQuery(name = "PersistentTag.findAll", query = "select o from PersistentTag o")
})
@Table(name = "t_tag")
/**
 * @deprecated: We're removing Derby DB from the ACM and are switching to a Lucene index
 *              for storing and searching all metadata.
 */
@Deprecated
class PersistentTag extends PersistentObject {
    private static final String COLUMN_VALUE = "gen_tag";

    @TableGenerator(name = COLUMN_VALUE,
            table = PersistentObject.SEQUENCE_TABLE_NAME,
            pkColumnName = PersistentObject.SEQUENCE_KEY,
            valueColumnName = PersistentObject.SEQUENCE_VALUE,
            pkColumnValue = COLUMN_VALUE,
            allocationSize = PersistentObject.ALLOCATION_SIZE)
    @Column(name = "id", nullable = false)
    @Id @GeneratedValue(generator = COLUMN_VALUE)
    private Integer id;

    @Column(name="uuid", nullable = false)
    private String uuid;

    @Column(name="name")
    private String name;

    @ManyToMany
    @JoinTable(
            name = "t_audioitem_has_tag",
            inverseJoinColumns = {
                    @JoinColumn(name = "audioitem", referencedColumnName = "id"),
            },
            joinColumns =
            @JoinColumn(name = "tag", referencedColumnName = "id")
            )
    private List<PersistentAudioItem> persistentAudioItemList = new LinkedList<PersistentAudioItem>();

    public PersistentTag() {
    }

    public PersistentTag(String name, String uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    public Integer getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setTitle(String name) {
        this.name = name;
    }

    public List<PersistentAudioItem> getPersistentAudioItemList() {
        return persistentAudioItemList;
    }

    @Override
    public String toString() {
        return uuid;
    }

    public static PersistentTag getFromDatabase(int id) {
        return PersistentQueries.getPersistentObject(PersistentTag.class, id);
    }

    public static PersistentTag getFromDatabase(String uuid) {
        EntityManager em = ACMConfiguration.getInstance().getCurrentDB().getEntityManager();
        PersistentTag result = null;
        try {
            Query findObject = em.createQuery("SELECT o FROM PersistentTag o WHERE o.uuid = '" + uuid + "'");
            result = (PersistentTag) findObject.getSingleResult();
        } catch (NoResultException e) {
            // do nothing
        } finally {
            em.close();
        }
        return result;
    }

    public static List<PersistentTag> getFromDatabase() {
        return PersistentQueries.getPersistentObjects(PersistentTag.class);
    }

    public static List<Playlist> toPlaylists(Collection<PersistentTag> tags) {
        List<Playlist> playlists = Lists.newArrayListWithCapacity(tags.size());
        for (PersistentTag tag : tags) {
            playlists.add(convert(tag));
        }
        return playlists;
    }

    public static Playlist convert(PersistentTag mItem) {
        List<AudioItemAndPosition> sorted = Lists.newArrayList();

        for (PersistentAudioItem audioItem : mItem.getPersistentAudioItemList()) {
            int position = PersistentTagOrdering.getFromDatabase(audioItem.getUuid(), mItem).getPosition();
            sorted.add(new AudioItemAndPosition(audioItem, position));
        }

        Collections.sort(sorted);

        Playlist playlist = new Playlist(mItem.getUuid());
        playlist.setName(mItem.getName());
        for (AudioItemAndPosition item : sorted) {
            playlist.addAudioItem(item.item.getUuid());
        }
        return playlist;
    }

    public static Integer uidToId(String uuid) {
        PersistentTag tag = PersistentTag.getFromDatabase(uuid);
        if (tag == null) {
            return null;
        }
        return tag.getId();
    }

    private static final class AudioItemAndPosition implements Comparable<AudioItemAndPosition> {
        final PersistentAudioItem item;
        final int position;

        AudioItemAndPosition(PersistentAudioItem item, int position) {
            this.item = item;
            this.position = position;
        }

        @Override public int compareTo(AudioItemAndPosition o) {
            return Integer.compare(position, o.position);
        }
    }
}
