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

import com.jbion.riseoflords.model.AccountState;
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

    public static int parseGoldStolen(String attackReportResponse) {
        Element body = Jsoup.parse(attackReportResponse).body();
        Elements elts = body.getElementsByAttributeValue("class", "combat_gagne");
        if (elts.size() == 0) {
            return 0;
        }
        Element divVictory = elts.get(0).parent().parent();
        return getTextAsNumber(divVictory.getElementsByTag("b").get(0));
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
        player.setRank(getTextAsNumber(rankElt));
        
        // name
        Element nameElt = fields.get(2).child(0);
        player.setName(nameElt.text().trim());
        
        // gold
        Element goldElt = fields.get(3);
        if (goldElt.hasText()) {
            // gold amount is textual
            player.setGold(getTextAsNumber(goldElt));
        } else {
            // gold amount is an image
            player.setGold(getGoldFromImgElement(goldElt.child(0)));
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
    
    private static int getTextAsNumber(Element numberElement) {
        String number = numberElement.text().trim();
        return Integer.valueOf(number.replace(".", ""));
    }

    public static void updateState(AccountState state, String response) {
        Element body = Jsoup.parse(response).body();
        state.gold = findValueInTag(body, "onmouseover", "A chaque tour de jeu");
        state.chestGold = findValueInTag(body, "onmouseover", "Votre coffre magique");
        state.mana = findValueInTag(body, "onmouseover", "Votre mana représente");
        state.turns = findValueInTag(body, "onmouseover", "Un nouveau tour de jeu");
        state.adventurins = findValueInTag(body, "href", "main/aventurines_detail");
    }
    
    private static int findValueInTag(Element root, String attrKey, String attrContains) {
        Elements elts = root.getElementsByAttributeValueContaining(attrKey, attrContains);
        String imgSrc = elts.get(0).child(0).attr("src");
        // get num param
        int position = imgSrc.indexOf("num=");
        if (position != -1) {
            String num = imgSrc.substring(position + 4);
            // remove other params
            int andPosition = num.indexOf("&");
            if (andPosition != -1) {
                num = num.substring(0, andPosition);
            }            
            return Integer.valueOf(num.replace(".", ""));
        } else {
            return 0;
        }
    }

    public static int parsePlayerGold(String playerPageResponse) {
        Element body = Jsoup.parse(playerPageResponse).body();
        Elements elts = body.getElementsByAttributeValueContaining("src", "aff_montant");
        Element img = elts.get(0);
        return getGoldFromImgElement(img);
    }
    
    private static int getGoldFromImgElement(Element goldImageElement) {
        assert goldImageElement.tagName().equals("img");
        String goldImgUrl = goldImageElement.attr("src");
        assert goldImgUrl.length() > 0 : "emtpy gold image url";
        try {
            BufferedImage img = ImageIO.read(new URL(BASE_URL + goldImgUrl));
            return GoldImageOCR.readAmount(img);
        } catch (IOException e) {
            System.err.println("error downloading image from url=" + goldImgUrl);
        }
        return -1;
    }
}
