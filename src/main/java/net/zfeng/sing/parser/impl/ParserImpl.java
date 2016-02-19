package net.zfeng.sing.parser.impl;

import net.zfeng.sing.exception.SingDataException;
import net.zfeng.sing.model.Musician;
import net.zfeng.sing.model.MusicianList;
import net.zfeng.sing.model.Song;
import net.zfeng.sing.model.SongList;
import net.zfeng.sing.parser.IParser;
import net.zfeng.sing.parser.config.SingCategory;
import net.zfeng.sing.parser.config.SingType;
import net.zfeng.sing.parser.config.SingUrl;
import net.zfeng.sing.utils.SingUrls;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhaofeng on 16/2/4.
 */
public class ParserImpl implements IParser {
    private static String SingIdRE = "^/m/detail/\\w+-([0-9]+)-\\d+.html$";
    private static String UserIdRE = "^/m/space/([0-9]+).html$";

    /**
     * 加载并解析指定url的html
     *
     * @param url
     * @return
     * @throws IOException
     */
    private Document parserHTML(String url) throws IOException {
        return Jsoup.connect(url).get();
    }

    /**
     * 删除html中的其他字符
     *
     * @param str
     * @return
     */
    public String deleteExtraChar(String str) {
        return str.replaceAll("&nbsp;", "").replaceAll(",", "").replaceAll("<br>", "").replaceAll(" ", "");
    }

    /**
     * 获取5sing歌曲列表
     *
     * @param page
     * @param type
     * @param category
     * @return
     */
    public SongList getSongs(SingType type, SingCategory category, int page) throws SingDataException {
        List<Song> songs = new ArrayList<Song>();
        SongList songList = new SongList();
        Pattern pattern = Pattern.compile(SingIdRE);
        try {
            Document doc = parserHTML(SingUrls.getURL(SingUrl.SONG_LIST, type, category, page, null));
            Elements songListEl = doc.getElementById("yc_list_tj").getElementsByTag("li");
            int i = 0;
            while (i++ < songListEl.size() - 1) {
                Element songEl = songListEl.get(i);
                Element nameEl = songEl.getElementsByTag("h4").get(0);
                Element authorEl = nameEl.getElementsByTag("strong").size() > 0 ? nameEl.getElementsByTag("strong").get(0) : null;
                Elements otherEls = songEl.getElementsByTag("span");
                String hrefVal = songEl.getElementsByTag("a").get(1).attr("href");

                Matcher matcher = pattern.matcher(hrefVal);

                Song song = new Song();

                if (authorEl != null) {
                    song.setAuthor(authorEl.text());

                    //删除歌曲名字中的作者信息
                    authorEl.remove();
                }

                if (matcher.find() && matcher.group().length() > 1) {
                    song.setId(matcher.group(1));
                }

                song.setName(nameEl.text());

                if (otherEls.size() > 1) {
                    int clickNumber = Integer.parseInt(deleteExtraChar(otherEls.get(0).text()).split(":")[1]);
                    int likeNumber = Integer.parseInt(deleteExtraChar(otherEls.get(1).text()).split(":")[1]);
                    song.setClickNumber(clickNumber);
                    song.setLikeNumber(likeNumber);
                }

                songs.add(song);
            }
        } catch (IOException e) {
            throw new SingDataException("从五婶获取数据失败\n" + e.getMessage());
        }
        songList.setList(songs);
        songList.setPage(page);
        return songList;
    }

    /**
     * 获取歌曲详情
     *
     * @param type
     * @param page
     * @param songId
     * @return
     */
    public Song getSong(SingType type, int page, String songId) throws SingDataException {
        Song song = new Song();

        try {
            Document doc = parserHTML(SingUrls.getURL(SingUrl.SONG_DETAIL, type, null, page, songId));
            Element headEl = doc.getElementsByClass("info_con").get(0).getElementsByTag("img").get(0);
            Elements infoEls = doc.getElementsByClass("info_con").get(0).getElementsByTag("span");
            Element titleEl = doc.getElementsByClass("m_title").get(0);
            Element mp3El = doc.getElementById("myPlayer");
            Element introductionEl = doc.getElementsByClass("info_txt").get(0);
            Element lrcEl = doc.getElementsByClass("info_txt").get(1);

            song.setId(songId);
            song.setName(titleEl.text().split(" - ")[1]);
            song.setAddress(mp3El.attr("src"));
            song.setIntroduction(introductionEl.text());
            song.setLrcs(lrcEl.text());

            if (infoEls.size() > 4) {
                String author = deleteExtraChar(infoEls.get(4).text()).split(":")[1];
                int clickNumber = Integer.parseInt(deleteExtraChar(infoEls.get(5).text()).split(":")[1]);
                int downNumber = Integer.parseInt(deleteExtraChar(infoEls.get(3).text()).split(":")[1]);
                String style = deleteExtraChar(infoEls.get(4).text()).split(":")[1];

                song.setAuthorImg(headEl.attr("src"));
                song.setAuthor(author);
                song.setClickNumber(clickNumber);
                song.setDownNumber(downNumber);
                song.setStyle(style);
            }
        } catch (IOException e) {
            throw new SingDataException("从五婶获取数据失败\n" + e.getMessage());
        }

        return song;
    }

    /**
     * 获取热门歌手列表
     *
     * @param page
     * @return
     */
    public MusicianList getMusicians(int page) throws SingDataException {
        MusicianList musicianList = new MusicianList();
        List<Musician> musicians = new ArrayList<Musician>();
        Pattern userIdPattern = Pattern.compile(UserIdRE);

        try {
            Document doc = parserHTML(SingUrl.MUSICIAN_HOT.getText().replace("{page}", page + ""));
            Elements imgEls = doc.getElementById("photolist").getElementsByTag("a");
            int i = 0;
            while (i++ < imgEls.size() - 1) {
                Element imgEl = imgEls.get(i);
                Matcher userIdMatcher = userIdPattern.matcher(imgEl.attr("href"));

                Musician musician = new Musician();

                if (userIdMatcher.find() && userIdMatcher.group().length() > 1) {
                    musician.setId(userIdMatcher.group(1));
                }

                musician.setName(imgEl.getElementsByTag("span").get(0).text());
                musician.setImgAddress(imgEl.getElementsByTag("img").get(0).attr("src"));
                musicians.add(musician);
            }
        } catch (IOException e) {
            throw new SingDataException("从五婶获取数据失败\n" + e.getMessage());
        }

        musicianList.setList(musicians);
        musicianList.setPage(page);
        return musicianList;
    }

    /**
     * 获取个人歌曲列表
     *
     * @param authorId
     * @param type
     * @param page
     * @return
     * @throws SingDataException
     */
    public SongList getPersonalSongs(String authorId, SingType type, int page) throws SingDataException {
        List<Song> songs = new ArrayList<Song>();
        SongList songList = new SongList();
        String url = SingUrl.AUTHOR_SONG_LIST.getText().replace("{authorId}", authorId).replace("{type}", type.getText()).replace("{page}", page + "");

        try {
            Document doc = parserHTML(url);
            Elements songsEl = doc.getElementsByClass("list_items").get(0).getElementsByTag("li");
            int i = 0;
            while (i++ < songsEl.size() - 1) {
                Song song = new Song();

                Element songEl = songsEl.get(i);
                Element infoEl = songEl.getElementsByTag("strong").get(0);
                String infos = infoEl.text();

                String name = songEl.getElementsByTag("h4").get(0).ownText();

                song.setName(name);
                song.setClickNumber(Integer.parseInt(infos.split(" ")[0].split("：")[1]));
                song.setUploadTime(infos.split(" ")[1].split("：")[1]);

                songs.add(song);
            }
            songList.setList(songs);
        } catch (IOException e) {
            throw new SingDataException("从五婶获取数据失败\n" + e.getMessage());
        }
        return songList;
    }

    /**
     * 获取个人粉丝
     *
     * @param userId
     * @param page
     * @return
     * @throws SingDataException
     */
    public MusicianList getFuns(String userId, int page) throws SingDataException {
        MusicianList musicianList = new MusicianList();
        List<Musician> musicians = new ArrayList<Musician>();
        String url = SingUrl.AUTHOR_FUNS.getText().replace("{authorId}", userId).replace("{page}", page + "");
        Pattern userIdPattern = Pattern.compile(UserIdRE);

        try {
            Document doc = parserHTML(url);
            Elements funEls = doc.getElementsByClass("m_fans").get(0).getElementsByTag("li");
            int i = 0;
            while (i++ < funEls.size() - 1) {
                Element funEl = funEls.get(i);
                Musician musician = new Musician();

                String img = funEl.getElementsByTag("img").get(0).attr("src");
                String name = funEl.getElementsByTag("h6").get(0).ownText();
                String introduction = funEl.getElementsByTag("p").get(0).text();
                String hrefVal = funEl.getElementsByTag("a").get(0).attr("href");
                Matcher userIdMatcher = userIdPattern.matcher(hrefVal);

                if (userIdMatcher.find() && userIdMatcher.group().length() > 1) {
                    musician.setId(userIdMatcher.group(1));
                }
                musician.setImgAddress(img);
                musician.setName(name);
                musician.setIntroduction(introduction);

                musicians.add(musician);
            }

            musicianList.setList(musicians);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return musicianList;
    }
}
