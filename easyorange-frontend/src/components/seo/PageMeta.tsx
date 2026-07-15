import { Helmet } from 'react-helmet-async';

interface PageMetaProps {
    title: string;
    description?: string;
    ogImage?: string;
}

const SITE_NAME = 'EasyOrange';

export function PageMeta({ title, description, ogImage }: PageMetaProps) {
    const fullTitle = `${title} | ${SITE_NAME}`;
    return (
        <Helmet>
            <title>{fullTitle}</title>
            <meta property="og:title" content={fullTitle} />
            <meta property="og:site_name" content={SITE_NAME} />
            {description && <meta name="description" content={description} />}
            {description && <meta property="og:description" content={description} />}
            {ogImage && <meta property="og:image" content={ogImage} />}
        </Helmet>
    );
}
