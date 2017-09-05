from bs4 import BeautifulSoup
import codecs
import urllib2
import json
import pickle
from datetime import datetime

# {"popular": ["com.google.android.apps.maps", "com.google.earth"]}

def getApps(url):
    previous_apps = []
    previous_skipped_apps = []
    start_idx = 0
    size = 100
    app_type = 'free'
    apps, skipped_apps = getTopAppsData( url, start_idx, size, app_type )

    """
    while(True):
        apps, skipped_apps = getTopAppsData( url, start_idx, size, app_type )
        if apps == previous_apps and skipped_apps == previous_skipped_apps: break
        for app in apps:
            if app['category'].upper() not in fileHandlers:
                fileHandlers[app['category'].upper()] = codecs.open( '_'.join( ["apps", app['category'].lower()] ), 'ab', character_encoding, buffering = 0 )
            fileHandler = fileHandlers[app['category'].upper()]
            try:
                fileHandler.write( json.dumps( app ) + "\n" )
            except Exception as e:
                print( e )
        previous_apps = apps
        previous_skipped_apps = skipped_apps
        start_idx += size
        saveState()
    """


def getPageAsSoup( url, post_values ):
    """
    if post_values:
        data = urllib.parse.urlencode( post_values )
        data = data.encode( character_encoding )
        req = urllib.request.Request( url, data )
    else:
        req = url
    """
    try:
        #response = urllib2.urlopen( req )
        response = urllib2.urlopen( url )
    except Exception as e:
        print( "HTTPError with: ", url, "\t", e )
        return None
    the_page = response.read()
    soup = BeautifulSoup( the_page )
 
    return soup

def getTopAppsData( url, start, num, app_type ):
    values = {'start' : start,
              'num': num,
              'numChildren':'0',
              'ipf': '1',
              'xhr': '1'}
    soup = getPageAsSoup( url, values )
    if not soup: return [], []
 
    apps = []
    skipped_apps = []

# {"popular": ["com.google.android.apps.maps", "com.google.earth"]}
    for div in soup.findAll( 'div', {'class' : 'details'} ):
        title = div.find( 'a', {'class':'title'} )
        myurl = title.get('href')
        result =  myurl.split('=')[1]
        #popString = popString + "\"" + result  + "\"" + ", "
        print  "\"" + result  + "\"" + ", "

        """
        app_details = getAppDetails( title.get( 'href' ) )
        if app_details: apps.append( app_details )
        else: skipped_apps.append( title.get( 'href' ) )
        """
 
    return apps, skipped_apps

categories = ['BOOKS_AND_REFERENCE', 'BUSINESS', 'COMICS', 'COMMUNICATION', 'EDUCATION', 'ENTERTAINMENT', 'FINANCE', 'HEALTH_AND_FITNESS', 'LIBRARIES_AND_DEMO', 'LIFESTYLE', 'APP_WALLPAPER', 'MEDIA_AND_VIDEO', 'MEDICAL', 'MUSIC_AND_AUDIO', 'NEWS_AND_MAGAZINES', 'PERSONALIZATION', 'PHOTOGRAPHY', 'PRODUCTIVITY', 'SHOPPING', 'SOCIAL', 'SPORTS', 'TOOLS', 'TRANSPORTATION', 'TRAVEL_AND_LOCAL', 'WEATHER', 'ARCADE', 'BRAIN', 'CARDS', 'CASUAL', 'GAME_WALLPAPER', 'RACING', 'SPORTS_GAMES', 'GAME_WIDGETS']

app_types = ['free', 'paid']

global popString

popString = "{\"popular\": ["

for category, app_type in [( x, y ) for x in categories for y in app_types]:
    #print( "Type = ", app_type, " Cateory = ", category )
    url = 'https://play.google.com/store/apps/category/' + category + '/collection/topselling_' + app_type
    getApps( url )

popString = popString + "]}"

pf = open("popular.txt", "w")
pf.write(popString)
pf.close()

