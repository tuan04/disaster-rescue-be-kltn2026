const crypto = require('crypto');

function generatePostId(groupId, content) {

    const normalizedContent = content
        .toLowerCase()
        .replace(/\s+/g, ' ')
        .trim();

    const hash = crypto
        .createHash('sha256')
        .update(`${groupId}:${normalizedContent}`)
        .digest('hex')
        .substring(0, 20);

    return `generated-${hash}`;
}


// ======================================================
// TÌM POST ID THẬT
// ======================================================

function extractPostId(links) {

    for (const link of links) {

        try {

            const url = new URL(link);

            // ------------------------------------------
            // Dạng:
            // /groups/123456/posts/987654
            // ------------------------------------------

            const postMatch = url.pathname.match(
                /\/groups\/[^/]+\/posts\/(\d+)/
            );

            if (postMatch) {

                return {
                    postId: postMatch[1],
                    isGenerated: false
                };
            }


            // ------------------------------------------
            // Dạng:
            // ?set=pcb.987654
            // ------------------------------------------

            const setParam =
                url.searchParams.get('set');

            if (
                setParam &&
                setParam.startsWith('pcb.')
            ) {

                return {
                    postId:
                        setParam.replace(
                            'pcb.',
                            ''
                        ),

                    isGenerated: false
                };
            }

        } catch (_) {
            // bỏ qua link lỗi
        }
    }

    return null;
}

module.exports = { generatePostId, extractPostId };
