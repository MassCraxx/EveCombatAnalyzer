public class Hit {

    private final int damage;
    private final String enemy;
    private final String weapon;
    private final Quality quality;

    public Hit(int damage, String enemy, String weapon, Quality quality) {
        this.damage = damage;
        int playerTag = enemy.indexOf('[');
        if(playerTag > 0){
            enemy = enemy.substring(0,playerTag);
        }
        this.enemy = enemy;
        this.weapon = weapon;
        this.quality = quality;
    }

    @Override
    public String toString() {
        return "Damage: " + damage + " Enemy: " + enemy + " Weapon: " + weapon + " Quality: " + quality;
    }

    public int getDamage() {
        return damage;
    }

    public String getEnemy() {
        return enemy;
    }

    public String getWeapon() {
        return weapon;
    }

    public Quality getQuality() {
        return quality;
    }

    public boolean isMiss(){
        return quality.equals(Quality.MISS);
    }

    public enum Quality {
        WRECKS,
        SMASHES,
        PENETRATES,
        HITS,
        GLANCES,
        GRAZES,
        MISS;

        @Override
        public String toString() {
            return this.name().charAt(0) + name().substring(1).toLowerCase();
        }
    }
}
