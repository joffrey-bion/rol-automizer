package com.jbion.riseoflords.network.parsers;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.jbion.riseoflords.model.Alignment;
import com.jbion.riseoflords.model.Army;
import com.jbion.riseoflords.model.Player;

public class Parser {

    private static final String BASE_URL = "http://www.riseoflords.com/";

    /**
     * Parses the Chest page response to return the current amount of gold.
     * 
     * @param chestPageResponse
     *            chest page response
     * @return the current amount of gold of the player
     */
    public static int parseGoldAmount(String chestPageResponse) {
        Element body = Jsoup.parse(chestPageResponse).body();
        Elements elts = body.getElementsByAttributeValue("name", "ArgentAPlacer");
        Element input = elts.get(0);
        String value = input.attr("value");
        return Integer.valueOf(value);
    }
    
    public static List<Player> parseUserList(String userListPageResponse) {
        Element body = Jsoup.parse(userListPageResponse).body();
        Elements elts = body.getElementsByAttributeValueContaining("href", "main/fiche&voirpseudo=");
        List<Player> list = new LinkedList<>();
        for (Element elt : elts) {
            Element usernameCell = elt.parent();
            assert usernameCell.tagName().equals("td");
            Element userRow = usernameCell.parent();
            assert userRow.tagName().equals("tr");
            list.add(parseUser(userRow));
        }
        return list;
    }
    
    private static Player parseUser(Element userRow) {
        assert userRow.tagName().equals("tr");
        Elements fields = userRow.getElementsByTag("td");
        Player player = new Player();
        
        // rank
        Element rankElt = fields.get(0);
        String rankStr = rankElt.text().trim();
        player.setRank(Integer.valueOf(rankStr));
        
        // name
        Element nameElt = fields.get(2).child(0);
        String name = nameElt.text().trim();
        player.setName(name);
        
        // gold
        Element goldElt = fields.get(3);
        if (goldElt.hasText()) {
            // gold amount is textual
            String gold = goldElt.text().trim();
            player.setGold(Integer.valueOf(gold.replace(".", "")));
        } else {
            // gold amount is an image
            Element goldImgElt = goldElt.child(0);
            assert goldImgElt.tagName().equals("img");
            String goldImgUrl = goldImgElt.attr("src");
            assert goldImgUrl.length() > 0 : "emtpy gold image url";
            int goldAmount = -1;
            try {
                BufferedImage img = ImageIO.read(new URL(BASE_URL + goldImgUrl));
                goldAmount = GoldImageOCR.readAmount(img);
            } catch (IOException e) {
                System.err.println("error downloading image from url=" + goldImgUrl);
            }
            player.setGold(goldAmount);
        }
        
        // army
        Element armyElt = fields.get(4);
        String army = armyElt.text().trim();
        player.setArmy(Army.get(army));
        
        // alignment
        Element alignmentElt = fields.get(5).child(0);
        String alignment = alignmentElt.text().trim();
        player.setAlignment(Alignment.get(alignment));
        
        return player;
    }

    /**
     * Parses the weapons page response to return the current state of the weapons.
     * 
     * @param weaponsPageResponse
     *            weapons page response
     * @return the current percentage of wornness of the weapons
     */
    public static int parseWeaponsWornness(String weaponsPageResponse) {
        Element body = Jsoup.parse(weaponsPageResponse).body();
        Elements elts = body.getElementsByAttributeValueContaining("title", "Armes endommagées");
        Element input = elts.get(0);
        String value = input.text().trim();
        if (value.endsWith("%")){
            return Integer.valueOf(value.substring(0, value.length() - 1));
        } else {
            return -1;
        }
    }
}
