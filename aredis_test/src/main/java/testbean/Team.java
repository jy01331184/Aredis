package testbean;

import java.util.List;

import aredis.persist.APersist;

/**
 * Created by tianyang on 17/12/27.
 */
public class Team implements APersist {

    public float slary;

    public boolean playoff;

    public String name;

    public int cham;

    public List<Player> players;

    @Override
    public String toString() {
        return "Team{" +
                "slary=" + slary +
                ", playoff=" + playoff +
                ", name='" + name + '\'' +
                ", cham=" + cham +
                ", players=" + players +
                '}';
    }
}
