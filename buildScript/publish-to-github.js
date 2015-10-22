/* eslint-env node */

var publishRelease = require('publish-release');
var fs = require('fs');
var exec = require('child_process').exec;
var request = require('request')


var args = JSON.parse( process.argv[2] || '{}' );
console.log(args);

if (args.tag && args.assets) {

    args.assets.forEach( function (fname) {
        if (!fs.existsSync(fname)) {
            console.log( 'Publish Release aborted!  Assets does not exist: ' + fname );
            process.exit(1);
        }
    });

    var rel_config = {
        token: 'a91d89f1d9abdb2c0b3ea0bd15747474eec93efc',
        owner: 'lsst',
        repo: 'firefly',
        tag: '',
        name: args.tag,
        draft: false,
        prerelease: true,
        assets: [],
        apiUrl: 'https://api.github.com'
    };

    rel_config = Object.assign(rel_config, args);
    console.log( 'rel_config: ' + JSON.stringify(rel_config, null, 2) );

    if (rel_config.notes) {
        doPublish(rel_config);
    } else {
        checkGitHub(rel_config, getChangeLog);
    }

} else {
    console.log('Publish Release aborted!  Missing required arguments: tag and assets.');
    process.exit(1);
}

function getChangeLog(rel_config, lastdate) {
    var cmd = 'git log --pretty=format:"- %s%n%b" --since="' + lastdate +'"';

    // push changes to github..
    exec(cmd, function (error, stdout, stderr) {
        rel_config.notes = 'Changelog: \n\n' + stdout;
        doPublish(rel_config);
    });
}

/**
 * check with github for latest commit datetime
 */
function checkGitHub(rel_config, callback) {

    request({
        uri: rel_config.apiUrl + '/repos/' + rel_config.owner + '/' + rel_config.repo + '/commits/master',
        method: 'GET',
        headers: {
            'Authorization': 'token ' + rel_config.token,
            'User-Agent': 'firely Jenkins'
        }
    }, function (err, res, body) {
        if (err) return callback(err)
        var result = JSON.parse(body);
        console.log( 'github lastest commit: ' + result.sha );
        callback(rel_config, result.commit.author.date);
    });
}

function doPublish(rel_config) {
    console.log( 'rel_config: ' + JSON.stringify(rel_config, null, 2) );

    exec('git checkout master');
    exec('git remote add lsst https://a91d89f1d9abdb2c0b3ea0bd15747474eec93efc@github.com/lsst/firefly.git');
    exec('git push --tags lsst master:test');


    publishRelease(rel_config,
        function (err, release) {
            if (err) {
                console.log('Failed: ' + JSON.stringify(err, null, 2));
            } else {
                if (release.html_url) {
                    console.log('Publish Done: ' + release.html_url);
                }
            }
        });
}
