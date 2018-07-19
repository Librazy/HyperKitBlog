package org.librazy.demo.dubbo.domain;

import org.librazy.demo.dubbo.config.JpaCryptoConverter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table
public class SrpAccountEntity implements Serializable {

    private static final long serialVersionUID = -6795256262794702197L;

    @Id
    @Column(unique = true, nullable = false)
    private Long id;

    @Version
    @Column(nullable = false)
    private Timestamp version;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String salt;

    /**
     * Note that we encrypt the verifier in the database to protect against
     * leaked database backups being used to perform an offline dictionary
     * attack
     */
    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String verifier;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id", nullable = false)
    private UserEntity user;

    protected SrpAccountEntity() {
    }

    public SrpAccountEntity(UserEntity user, String salt, String verifier) {
        this.user = user;
        user.setSrpAccount(this);
        this.id = user.getId();
        this.salt = salt;
        this.verifier = verifier;
    }

    // this models people guessing emails and being given a random salt
    public SrpAccountEntity(UserEntity user, String salt) {
        this.user = user;
        user.setSrpAccount(this);
        this.id = user.getId();
        this.salt = salt;
        this.verifier = null;
    }

    public Long getId() {
        return id;
    }


    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
        this.id = user.getId();
        user.setSrpAccount(this);
    }

    public String getVerifier() {
        return verifier;
    }

    public void setVerifier(String verifier) {
        this.verifier = verifier;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id.intValue();
        result = prime * result + ((salt == null) ? 0 : salt.hashCode());
        result = prime * result
                         + ((verifier == null) ? 0 : verifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SrpAccountEntity other = (SrpAccountEntity) obj;
        if (!id.equals(other.id))
            return false;
        if (salt == null) {
            if (other.salt != null)
                return false;
        } else if (!salt.equals(other.salt))
            return false;
        if (verifier == null) {
            return other.verifier == null;
        } else return verifier.equals(other.verifier);
    }

    @Override
    public String toString() {
        return "SrpAccountEntity [email=" + id + ", salt="
                       + salt + ", verifier=" + verifier + "]";
    }

    public Timestamp getVersion() {
        return version;
    }

    protected void setVersion(Timestamp version) {
        this.version = version;
    }
}
