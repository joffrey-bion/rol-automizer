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
import com.jbion.riseoflords.model.User;

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
    
    public static List<User> parseUserList(String userListPageResponse) {
        Element body = Jsoup.parse(userListPageResponse).body();
        Elements elts = body.getElementsByAttributeValueContaining("href", "main/fiche&voirpseudo=");
        List<User> list = new LinkedList<User>();
        for (Element elt : elts) {
            Element usernameCell = elt.parent();
            assert usernameCell.tagName().equals("td");
            Element userRow = usernameCell.parent();
            assert userRow.tagName().equals("tr");
            list.add(parseUser(userRow));
        }
        return list;
    }
    
    private static User parseUser(Element userRow) {
        assert userRow.tagName().equals("tr");
        Elements fields = userRow.getElementsByTag("td");
        User user = new User();
        
        // rank
        Element rankElt = fields.get(0);
        String rankStr = rankElt.text().trim();
        user.setRank(Integer.valueOf(rankStr));
        
        // name
        Element nameElt = fields.get(2).child(0);
        String name = nameElt.text().trim();
        user.setName(name);
        
        // gold
        Element goldElt = fields.get(3);
        if (goldElt.hasText()) {
            // gold amount is textual
            String gold = goldElt.text().trim();
            user.setGold(Integer.valueOf(gold.replace(".", "")));
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
            user.setGold(goldAmount);
        }
        
        // army
        Element armyElt = fields.get(4);
        String army = armyElt.text().trim();
        user.setArmy(Army.get(army));
        
        // alignment
        Element alignmentElt = fields.get(5).child(0);
        String alignment = alignmentElt.text().trim();
        user.setAlignment(Alignment.get(alignment));
        
        return user;
    }
}
